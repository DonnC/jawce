import { Chat } from '@/types/chat';
import { ChatListItem } from './ChatListItem';
import { MessageSquare, Clock, User } from 'lucide-react';

interface ChatSidebarProps {
    pendingChats: Chat[];
    activeChats: Chat[];
    selectedChatId: string | null;
    onSelectChat: (chatId: string) => void;
    agentName: string;
}

export function ChatSidebar({
                                pendingChats,
                                activeChats,
                                selectedChatId,
                                onSelectChat,
                                agentName
                            }: ChatSidebarProps) {
    return (
        <aside className="w-80 bg-sidebar flex flex-col h-full border-r border-sidebar-border">
            {/* Header */}
            <div className="p-4 border-b border-sidebar-border">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-whatsapp flex items-center justify-center">
                        <MessageSquare className="w-5 h-5 text-white" />
                    </div>
                    <div>
                        <h1 className="text-lg font-bold text-sidebar-foreground">Jawce</h1>
                        <p className="text-xs text-sidebar-foreground/60">Live Support</p>
                    </div>
                </div>
            </div>

            {/* Agent Info */}
            <div className="px-4 py-3 border-b border-sidebar-border bg-sidebar-accent/50">
                <div className="flex items-center gap-2">
                    <User className="w-4 h-4 text-sidebar-foreground/60" />
                    <span className="text-sm text-sidebar-foreground/80">{agentName}</span>
                    <span className="ml-auto w-2 h-2 bg-online rounded-full" />
                </div>
            </div>

            {/* Chat Lists */}
            <div className="flex-1 overflow-y-auto scrollbar-thin">
                {/* Pending Queue */}
                <div className="p-3">
                    <div className="flex items-center gap-2 px-2 mb-2">
                        <Clock className="w-4 h-4 text-pending" />
                        <span className="text-xs font-semibold text-sidebar-foreground/70 uppercase tracking-wide">
              Pending Queue
            </span>
                        {pendingChats.length > 0 && (
                            <span className="ml-auto px-2 py-0.5 bg-pending/20 text-pending text-xs font-medium rounded-full">
                {pendingChats.length}
              </span>
                        )}
                    </div>

                    {pendingChats.length === 0 ? (
                        <p className="text-sm text-sidebar-foreground/50 px-2 py-4 text-center">
                            No pending chats
                        </p>
                    ) : (
                        <div className="space-y-1">
                            {pendingChats.map(chat => (
                                <ChatListItem
                                    key={chat.id}
                                    chat={chat}
                                    isSelected={chat.id === selectedChatId}
                                    onClick={() => onSelectChat(chat.id)}
                                />
                            ))}
                        </div>
                    )}
                </div>

                {/* Active Chats */}
                <div className="p-3 border-t border-sidebar-border">
                    <div className="flex items-center gap-2 px-2 mb-2">
                        <MessageSquare className="w-4 h-4 text-online" />
                        <span className="text-xs font-semibold text-sidebar-foreground/70 uppercase tracking-wide">
              My Active Chats
            </span>
                        {activeChats.length > 0 && (
                            <span className="ml-auto px-2 py-0.5 bg-online/20 text-online text-xs font-medium rounded-full">
                {activeChats.length}
              </span>
                        )}
                    </div>

                    {activeChats.length === 0 ? (
                        <p className="text-sm text-sidebar-foreground/50 px-2 py-4 text-center">
                            No active chats
                        </p>
                    ) : (
                        <div className="space-y-1">
                            {activeChats.map(chat => (
                                <ChatListItem
                                    key={chat.id}
                                    chat={chat}
                                    isSelected={chat.id === selectedChatId}
                                    onClick={() => onSelectChat(chat.id)}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </aside>
    );
}
