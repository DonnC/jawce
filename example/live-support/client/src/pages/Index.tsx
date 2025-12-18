import { ChatSidebar } from '@/components/ChatSidebar';
import { ChatWindow } from '@/components/ChatWindow';
import { useChatStore } from '@/hooks/useChatStore';

const Index = () => {
    const {
        pendingChats,
        activeChats,
        selectedChat,
        selectedMessages,
        selectedChatId,
        currentAgent,
        selectChat,
        claimChat,
        closeChat,
        sendMessage,
    } = useChatStore();

    return (
        <div className="flex h-screen overflow-hidden">
            <ChatSidebar
                pendingChats={pendingChats}
                activeChats={activeChats}
                selectedChatId={selectedChatId}
                onSelectChat={selectChat}
                agentName={currentAgent.name}
            />
            <ChatWindow
                chat={selectedChat}
                messages={selectedMessages}
                onClaim={claimChat}
                onClose={closeChat}
                onSendMessage={sendMessage}
            />
        </div>
    );
};

export default Index;
