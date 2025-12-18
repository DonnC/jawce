import * as React from "react";
import { cn } from "@/lib/utils";

export interface VariableTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {}

const VariableTextarea = React.forwardRef<HTMLTextAreaElement, VariableTextareaProps>(
  ({ className, value, onChange, ...props }, ref) => {
    const [displayValue, setDisplayValue] = React.useState(value || '');
    
    React.useEffect(() => {
      setDisplayValue(value || '');
    }, [value]);

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setDisplayValue(e.target.value);
      onChange?.(e);
    };

    // Render text with highlighted variables
    const renderHighlightedText = () => {
      const text = String(displayValue);
      const parts = text.split(/({{[^}]*}})/g);
      
      return parts.map((part, index) => {
        if (part.match(/^{{[^}]*}}$/)) {
          return (
            <span key={index} className="bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 px-1 rounded">
              {part}
            </span>
          );
        }
        return <span key={index}>{part}</span>;
      });
    };

    return (
      <div className="relative">
        {/* Hidden overlay for highlighting - shown behind textarea */}
        <div
          className={cn(
            "absolute inset-0 pointer-events-none rounded-md border border-transparent px-3 py-2 text-sm whitespace-pre-wrap break-words opacity-0",
            className
          )}
          style={{
            font: 'inherit',
            lineHeight: 'inherit',
          }}
          aria-hidden="true"
        >
          {renderHighlightedText()}
        </div>
        
        {/* Actual textarea */}
        <textarea
          ref={ref}
          value={displayValue}
          onChange={handleChange}
          className={cn(
            "flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 relative",
            className
          )}
          style={{
            background: 'transparent',
          }}
          {...props}
        />
        
        {/* Highlighted overlay on top */}
        <div
          className={cn(
            "absolute inset-0 pointer-events-none rounded-md px-3 py-2 text-sm whitespace-pre-wrap break-words overflow-hidden",
          )}
          style={{
            font: 'inherit',
            lineHeight: 'inherit',
            color: 'transparent',
          }}
          aria-hidden="true"
        >
          {renderHighlightedText()}
        </div>
      </div>
    );
  }
);

VariableTextarea.displayName = "VariableTextarea";

export { VariableTextarea };
