export type ChatStatus = 'PENDING' | 'ACTIVE' | 'CLOSED';

export interface Message {
  id: string;
  chatId: string;
  content: string;
  sender: 'agent' | 'user';
  timestamp: Date;
}

export interface Chat {
  id: string;
  customerName: string;
  customerPhone: string;
  status: ChatStatus;
  assignedAgent: string | null;
  agentName: string | null;
  createdAt: Date;
  lastMessage: string;
  lastMessageTime: Date;
  unreadCount: number;
}

// JSON structure for backend communication
export interface ApiRequest {
  action: 'CLAIM_CHAT' | 'RELEASE' | 'CLOSE_CHAT' | 'SEND_MESSAGE' | 'GET_CHATS' | 'GET_MESSAGES';
  chatId?: string;
  agentId?: string;
  message?: string;
}

export interface ApiResponse {
  success: boolean;
  data?: unknown;
  error?: string;
}
