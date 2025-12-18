import { Message } from '@/types/chat';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { Check, CheckCheck } from 'lucide-react';

interface MessageBubbleProps {
    message: Message;
}

export function MessageBubble({ message }: MessageBubbleProps) {
    const isAgent = message.sender === 'agent';

    return (
        <div
            className={cn(
                'flex animate-fade-in',
                isAgent ? 'justify-end' : 'justify-start'
            )}
        >
            <div
                className={cn(
                    'max-w-[70%] px-4 py-2.5 rounded-2xl shadow-sm',
                    isAgent
                        ? 'bg-chat-bubble-outgoing rounded-br-md'
                        : 'bg-chat-bubble-incoming rounded-bl-md'
                )}
            >
                <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">
                    {message.content}
                </p>
                <div className={cn(
                    'flex items-center gap-1 mt-1',
                    isAgent ? 'justify-end' : 'justify-start'
                )}>
          <span className="text-[10px] text-muted-foreground">
            {format(message.timestamp, 'HH:mm')}
          </span>
                    {isAgent && (
                        <CheckCheck className="w-3.5 h-3.5 text-whatsapp" />
                    )}
                </div>
            </div>
        </div>
    );
}
