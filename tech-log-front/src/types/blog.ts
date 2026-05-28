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
  images?: PostImage[];
  published?: boolean;
  views?: number;
}

export interface PostImage {
  url: string;
  originalName: string;
  storedName: string;
  order: number;
  thumbnail: boolean;
}
