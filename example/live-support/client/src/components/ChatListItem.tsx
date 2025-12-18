import { Chat } from '@/types/chat';
import { cn } from '@/lib/utils';
import { formatDistanceToNow } from 'date-fns';

interface ChatListItemProps {
    chat: Chat;
    isSelected: boolean;
    onClick: () => void;
}

export function ChatListItem({ chat, isSelected, onClick }: ChatListItemProps) {
    const initials = chat.customerName
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase();

    return (
        <button
            onClick={onClick}
            className={cn(
                'w-full flex items-center gap-3 p-3 rounded-lg transition-all duration-200 text-left',
                'hover:bg-sidebar-accent',
                isSelected && 'bg-sidebar-accent'
            )}
        >
            <div className="relative flex-shrink-0">
                <div className="w-12 h-12 rounded-full bg-whatsapp/20 flex items-center justify-center">
                    <span className="text-sm font-semibold text-whatsapp">{initials}</span>
                </div>
                {chat.status === 'PENDING' && (
                    <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-pending rounded-full border-2 border-sidebar animate-pulse-soft" />
                )}
                {chat.status === 'ACTIVE' && (
                    <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-online rounded-full border-2 border-sidebar" />
                )}
            </div>

            <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
          <span className="font-medium text-sidebar-foreground truncate">
            {chat.customerName}
          </span>
                    <span className="text-xs text-sidebar-foreground/60 flex-shrink-0">
            {formatDistanceToNow(chat.lastMessageTime, { addSuffix: false })}
          </span>
                </div>
                <div className="flex items-center justify-between gap-2 mt-0.5">
                    <p className="text-sm text-sidebar-foreground/70 truncate">
                        {chat.lastMessage}
                    </p>
                    {chat.unreadCount > 0 && (
                        <span className="flex-shrink-0 min-w-[20px] h-5 px-1.5 bg-whatsapp text-white text-xs font-medium rounded-full flex items-center justify-center">
              {chat.unreadCount}
            </span>
                    )}
                </div>
            </div>
        </button>
    );
}
