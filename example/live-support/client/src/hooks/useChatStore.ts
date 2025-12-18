import {useCallback, useState} from 'react';
import {ApiRequest, Chat, Message} from '@/types/chat';
import {currentAgent, initMessages} from '@/data/mockData';
import {toast} from '@/hooks/use-toast';
import {useWebSocket} from '@/hooks/useWebSocket';

// TODO: set the below field properly
// Toggle this to switch between mock and real WebSocket
// Your Spring Boot WebSocket URL
const USE_WEBSOCKET = true;
const WS_URL = 'ws://localhost:8000/chatbot/ws/chat';

export function useChatStore() {
    const [chats, setChats] = useState<Chat[]>([]);
    const [messages, setMessages] = useState<Record<string, Message[]>>(initMessages);
    const [selectedChatId, setSelectedChatId] = useState<string | null>(null);

    const handleIncomingMessage = useCallback((message: Message) => {
        console.log("Incoming message", message);

        setMessages(prev => ({
            ...prev,
            [message.chatId]: [...(prev[message.chatId] || []), message],
        }));

        setChats(prev => prev.map(chat =>
            chat.id === message.chatId
                ? {
                    ...chat,
                    lastMessage: message.content,
                    lastMessageTime: new Date(message.timestamp),
                    unreadCount: chat.unreadCount + 1
                }
                : chat
        ));
    }, []);

    const handleChatUpdate = useCallback((updatedChat: Chat) => {
        setChats(prev => prev.map(chat =>
            chat.id === updatedChat.id ? {...updatedChat, lastMessageTime: new Date(updatedChat.lastMessageTime)} : chat
        ));
    }, []);

    const handleChatList = useCallback((chatList: Chat[]) => {
        setChats(chatList.map(c => ({
            ...c,
            lastMessageTime: new Date(c.lastMessageTime),
        })));

        // Extract messages from each chat and populate messages state
        const extractedMessages: Record<string, Message[]> = {};
        for (const chat of chatList) {
            if (Array.isArray((chat as any).messages)) {
                extractedMessages[chat.id] = (chat as any).messages.map((m: any) => ({
                    ...m,
                    timestamp: new Date(m.timestamp),
                }));
            }
        }
        setMessages(extractedMessages);
    }, []);

    const {isConnected, send} = useWebSocket({
        url: USE_WEBSOCKET ? WS_URL : '',
        onMessage: handleIncomingMessage,
        onChatUpdate: handleChatUpdate,
        onChatList: handleChatList,
    });

    const selectedChat = chats.find(c => c.id === selectedChatId) || null;
    const selectedMessages = selectedChatId ? messages[selectedChatId] || [] : [];

    const pendingChats = chats.filter(c => c.status === 'PENDING');
    const activeChats = chats.filter(c => c.status === 'ACTIVE' && c.assignedAgent === currentAgent.id);

    // Send to backend (WebSocket or mock)
    const sendToBackend = useCallback((request: ApiRequest): Promise<boolean> => {
        console.log('📤 Sending to backend:', JSON.stringify(request, null, 2));

        if (USE_WEBSOCKET && isConnected) {
            send(request.action, request);
            return Promise.resolve(true);
        }

        // Mock mode - simulate network delay
        return new Promise(resolve => setTimeout(() => resolve(true), 300));
    }, [isConnected, send]);

    const claimChat = useCallback(async (chatId: string) => {
        const request: ApiRequest = {
            action: 'CLAIM_CHAT',
            chatId,
            agentId: currentAgent.id,
        };

        const success = await sendToBackend(request);

        if (success) {
            setChats(prev => prev.map(chat =>
                chat.id === chatId
                    ? {...chat, status: 'ACTIVE', assignedAgent: currentAgent.id, agentName: currentAgent.name}
                    : chat
            ));
            setSelectedChatId(chatId);
            toast({
                title: 'Chat claimed',
                description: 'You are now handling this conversation.',
            });
        }
    }, [sendToBackend]);

    const releaseChat = useCallback(async (chatId: string) => {
        const request: ApiRequest = {
            action: 'RELEASE',
            chatId,
            agentId: currentAgent.id,
        };

        const success = await sendToBackend(request);

        if (success) {
            setChats(prev => prev.map(chat =>
                chat.id === chatId
                    ? {...chat, status: 'PENDING', assignedAgent: null, agentName: null}
                    : chat
            ));
            if (selectedChatId === chatId) {
                setSelectedChatId(null);
            }
            toast({
                title: 'Chat released',
                description: 'Chat returned to the queue.',
            });
        }
    }, [sendToBackend, selectedChatId]);

    const closeChat = useCallback(async (chatId: string) => {
        const request: ApiRequest = {
            action: 'CLOSE_CHAT',
            chatId,
            agentId: currentAgent.id,
        };

        const success = await sendToBackend(request);

        if (success) {
            setChats(prev => prev.map(chat =>
                chat.id === chatId
                    ? {...chat, status: 'CLOSED'}
                    : chat
            ));
            if (selectedChatId === chatId) {
                setSelectedChatId(null);
            }
            toast({
                title: 'Chat closed',
                description: 'The conversation has been ended.',
            });
        }
    }, [sendToBackend, selectedChatId]);

    const sendMessage = useCallback(async (chatId: string, content: string) => {
        const request: ApiRequest = {
            action: 'SEND_MESSAGE',
            chatId,
            agentId: currentAgent.id,
            message: content,
        };

        const success = await sendToBackend(request);

        if (success) {
            // const newMessage: Message = {
            //     id: `msg-${Date.now()}`,
            //     chatId,
            //     content,
            //     sender: 'agent',
            //     timestamp: new Date(),
            // };
            //
            // setMessages(prev => ({
            //     ...prev,
            //     [chatId]: [...(prev[chatId] || []), newMessage],
            // }));

            setChats(prev => prev.map(chat =>
                chat.id === chatId
                    ? {...chat, lastMessage: content, lastMessageTime: new Date(), unreadCount: 0}
                    : chat
            ));
        }
    }, [sendToBackend]);

    const selectChat = useCallback((chatId: string | null) => {
        setSelectedChatId(chatId);
        if (chatId) {
            setChats(prev => prev.map(chat =>
                chat.id === chatId ? {...chat, unreadCount: 0} : chat
            ));
        }
    }, []);

    return {
        chats,
        pendingChats,
        activeChats,
        selectedChat,
        selectedMessages,
        selectedChatId,
        currentAgent,
        isConnected,
        selectChat,
        claimChat,
        releaseChat,
        closeChat,
        sendMessage,
    };
}
