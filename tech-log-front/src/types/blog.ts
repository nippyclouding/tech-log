export interface Post {
  id: string | number;
  title: string;
  excerpt: string;
  content: string;
  date: string;
  author: {
    name: string;
    avatar: string;
    role: string;
  };
  category: string;
  tags: string[];
  coverImage: string;
  published?: boolean;
  views?: number;
}
