import { useEffect, useRef } from 'react';
import { Chat, Message } from '@/types/chat';
import { ChatHeader } from './ChatHeader';
import { MessageBubble } from './MessageBubble';
import { MessageInput } from './MessageInput';
import { MessageSquare } from 'lucide-react';

interface ChatWindowProps {
  chat: Chat | null;
  messages: Message[];
  onClaim: (chatId: string) => void;
  onClose: (chatId: string) => void;
  onSendMessage: (chatId: string, message: string) => void;
}

export function ChatWindow({
                             chat,
                             messages,
                             onClaim,
                             onClose,
                             onSendMessage
                           }: ChatWindowProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (!chat) {
    return (
        <div className="flex-1 flex items-center justify-center bg-background">
          <div className="text-center">
            <div className="w-20 h-20 mx-auto mb-4 rounded-full bg-muted flex items-center justify-center">
              <MessageSquare className="w-10 h-10 text-muted-foreground" />
            </div>
            <h2 className="text-xl font-semibold text-foreground mb-2">Select a conversation</h2>
            <p className="text-muted-foreground">
              Choose a chat from the sidebar to start messaging
            </p>
          </div>
        </div>
    );
  }

  const canSendMessages = chat.status === 'ACTIVE';

  return (
      <div className="flex-1 flex flex-col bg-background">
        <ChatHeader
            chat={chat}
            onClaim={() => onClaim(chat.id)}
            onClose={() => onClose(chat.id)}
        />

        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCI+CjxyZWN0IHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgZmlsbD0iI2Y1ZjVmNSI+PC9yZWN0Pgo8Y2lyY2xlIGN4PSIzMCIgY3k9IjMwIiByPSIxIiBmaWxsPSIjZTBlMGUwIj48L2NpcmNsZT4KPC9zdmc+')]">
          {messages.map(message => (
              <MessageBubble key={message.id} message={message} />
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* Status Banner for Pending */}
        {chat.status === 'PENDING' && (
            <div className="px-4 py-3 bg-pending/10 border-t border-pending/20">
              <p className="text-sm text-center text-pending font-medium">
                This chat is unclaimed. Click "Claim Chat" to start responding.
              </p>
            </div>
        )}

        <MessageInput
            onSend={(message) => onSendMessage(chat.id, message)}
            disabled={!canSendMessages}
        />
      </div>
  );
}
