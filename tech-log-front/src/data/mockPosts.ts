import { Post } from "../types/blog";

export const MOCK_POSTS: Post[] = [
  {
    id: "1",
    title: "성장하는 개발자가 되기 위한 3가지 습관",
    excerpt: "성장은 단순히 기술적 지식을 쌓는 것 이상입니다. 어떻게 하면 지속 가능한 성장을 이룰 수 있을까요?",
    content: "# 성장하는 개발자가 되기 위한 3가지 습관\n\n성장은 단순히 기술적 지식을 쌓는 것 이상입니다. bagaimana 하면 지속 가능한 성장을 이룰 수 있을까요?...",
    date: "2024-03-15",
    author: {
      name: "김민재",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
      role: "Frontend Engineer"
    },
    category: "Career",
    tags: ["Growth", "Mindset", "Career"],
    coverImage: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop"
  },
  {
    id: "2",
    title: "React 19에서 변경되는 핵심 기능 살펴보기",
    excerpt: "React 19가 다가오고 있습니다. 컴파일러 도입부터 액션까지, 무엇이 달라지는지 미리 알아봅니다.",
    content: "# React 19에서 변경되는 핵심 기능 살펴보기\n\nReact 19가 다가오고 있습니다. 컴파일러 도입부터 액션까지, 무엇이 달라지는지 미리 알아봅니다.",
    date: "2024-03-10",
    author: {
      name: "이하늘",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka",
      role: "Web Developer"
    },
    category: "React",
    tags: ["React", "JavaScript", "Frontend"],
    coverImage: "https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=800&auto=format&fit=crop"
  },
  {
    id: "3",
    title: "대규모 트래픽을 견디는 아키텍처 설계기",
    excerpt: "시스템이 커짐에 따라 발생하는 다양한 문제들을 해결하며 배운 MSA와 분산 처리에 대한 노하우를 공유합니다.",
    content: "# 대규모 트래픽을 견디는 아키텍처 설계기\n\n시스템이 커짐에 따라 발생하는 다양한 문제들을 해결하며 배운 MSA와 분산 처리에 대한 노하우 공유합니다.",
    date: "2024-03-05",
    author: {
      name: "박준서",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Leo",
      role: "Backend Engineer"
    },
    category: "Architecture",
    tags: ["MSA", "Scalability", "Backend"],
    coverImage: "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop"
  },
  {
    id: "4",
    title: "디자인 시스템 구축: 첫걸음부터 완성까지",
    excerpt: "지속 가능한 UI 개발을 위한 디자인 시스템, 왜 필요하고 어떻게 시작해야 할까요?",
    content: "# 디자인 시스템 구축: 첫걸음부터 완성까지\n\n지속 가능한 UI 개발을 위한 디자인 시스템, 왜 필요하고 어떻게 시작해야 할까요?",
    date: "2024-02-28",
    author: {
      name: "정다은",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Daisy",
      role: "Product Designer"
    },
    category: "Design",
    tags: ["DesignSystem", "UI/UX", "Tailwind"],
    coverImage: "https://images.unsplash.com/photo-1581291518062-c9a79415c674?w=800&auto=format&fit=crop"
  }
];
