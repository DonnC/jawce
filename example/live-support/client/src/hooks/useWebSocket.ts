import {useEffect, useState, useCallback, useRef} from 'react';
import { Message, Chat } from '@/types/chat';

interface WebSocketMessage {
  type: 'MESSAGE' | 'CHAT_UPDATE' | 'CHAT_LIST' | 'CONNECTED';
  payload: any;
}

interface UseWebSocketOptions {
  url: string;
  onMessage?: (message: Message) => void;
  onChatUpdate?: (chat: Chat) => void;
  onChatList?: (chats: Chat[]) => void;
}

// ✅ module‑level singleton
let ws: WebSocket | null = null;
let reconnectTimeout: NodeJS.Timeout | null = null;

export function useWebSocket({ url, onMessage, onChatUpdate, onChatList }: UseWebSocketOptions) {
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const reconnectAttemptsRef = useRef(0);

  const connect = useCallback(() => {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      console.log('⚠️ WebSocket already connected or connecting');
      return;
    }

    try {
      ws = new WebSocket(url);

      ws.onopen = () => {
        console.log('✅ WebSocket connected');
        setIsConnected(true);
        setError(null);
      };

      ws.onmessage = (event) => {
        try {
          const data: WebSocketMessage = JSON.parse(event.data);
          console.log('📥 Received:', data);

          switch (data.type) {
            case 'MESSAGE':
              onMessage?.(data.payload);
              break;
            case 'CHAT_UPDATE':
              onChatUpdate?.(data.payload);
              break;
            case 'CHAT_LIST':
              onChatList?.(data.payload);
              break;
          }
        } catch (e) {
          console.error('Failed to parse message:', e);
        }
      };

      ws.onclose = () => {
        console.log('❌ WebSocket disconnected');
        setIsConnected(false);

        if (reconnectTimeout) clearTimeout(reconnectTimeout);

        const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, 30000); // max 30s
        reconnectAttemptsRef.current += 1;
        reconnectTimeout = setTimeout(connect, delay);
      };


      ws.onerror = (e) => {
        console.error('WebSocket error:', e);
        setError('Connection failed');
      };
    } catch (e) {
      setError('Failed to connect');
    }
  }, [url, onMessage, onChatUpdate, onChatList]);

  const send = useCallback((type: string, payload: any) => {
    if (ws?.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, payload });
      console.log('📤 Sending:', message);
      ws.send(message);
    } else {
      console.warn('WebSocket not connected');
    }
  }, []);

  const disconnect = useCallback(() => {
    if (reconnectTimeout) clearTimeout(reconnectTimeout);
    ws?.close();
    ws = null;
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { isConnected, error, send };
}