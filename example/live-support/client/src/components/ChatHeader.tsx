import { Chat } from '@/types/chat';
import { Button } from '@/components/ui/button';
import { Phone, UserCheck, XCircle } from 'lucide-react';

interface ChatHeaderProps {
    chat: Chat;
    onClaim: () => void;
    onClose: () => void;
}

export function ChatHeader({ chat, onClaim, onClose }: ChatHeaderProps) {
    const initials = chat.customerName
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase();

    const isPending = chat.status === 'PENDING';
    const isActive = chat.status === 'ACTIVE';

    return (
        <header className="h-16 px-4 flex items-center justify-between bg-card border-b border-border">
            <div className="flex items-center gap-3">
                <div className="relative">
                    <div className="w-10 h-10 rounded-full bg-whatsapp/20 flex items-center justify-center">
                        <span className="text-sm font-semibold text-whatsapp">{initials}</span>
                    </div>
                    <span className={cn(
                        'absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-card',
                        isPending ? 'bg-pending' : 'bg-online'
                    )} />
                </div>
                <div>
                    <h2 className="font-semibold text-foreground">{chat.customerName}</h2>
                    <p className="text-xs text-muted-foreground flex items-center gap-1">
                        <Phone className="w-3 h-3" />
                        {chat.customerPhone}
                    </p>
                </div>
            </div>

            <div className="flex items-center gap-2">
                {isPending && (
                    <Button onClick={onClaim} size="sm" className="gap-2">
                        <UserCheck className="w-4 h-4" />
                        Claim Chat
                    </Button>
                )}

                {isActive && (
                    <Button onClick={onClose} variant="destructive" size="sm" className="gap-2">
                        <XCircle className="w-4 h-4" />
                        Close Chat
                    </Button>
                )}
            </div>
        </header>
    );
}

function cn(...classes: (string | boolean | undefined)[]) {
    return classes.filter(Boolean).join(' ');
}
