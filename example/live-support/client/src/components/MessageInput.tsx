import { useState, KeyboardEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Send, Paperclip, Smile } from 'lucide-react';

interface MessageInputProps {
  onSend: (message: string) => void;
  disabled?: boolean;
}

export function MessageInput({ onSend, disabled }: MessageInputProps) {
  const [message, setMessage] = useState('');

  const handleSend = () => {
    if (message.trim() && !disabled) {
      onSend(message.trim());
      setMessage('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
      <div className="p-4 bg-card border-t border-border">
        <div className="flex items-end gap-2">
          <Button variant="ghost" size="icon" className="flex-shrink-0 text-muted-foreground hover:text-foreground">
            <Paperclip className="w-5 h-5" />
          </Button>
          <Button variant="ghost" size="icon" className="flex-shrink-0 text-muted-foreground hover:text-foreground">
            <Smile className="w-5 h-5" />
          </Button>

          <div className="flex-1 relative">
            <Textarea
                value={message}
                onChange={e => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={disabled ? 'Claim the chat to send messages...' : 'Type a message...'}
                disabled={disabled}
                className="min-h-[44px] max-h-32 resize-none pr-12 bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-ring"
                rows={1}
            />
          </div>

          <Button
              onClick={handleSend}
              disabled={!message.trim() || disabled}
              size="icon"
              className="flex-shrink-0 bg-whatsapp hover:bg-whatsapp-dark"
          >
            <Send className="w-5 h-5" />
          </Button>
        </div>
      </div>
  );
}
