import React, { useEffect, useState } from "react";
import { format } from "date-fns";
import { Send, MessageCircle, Github, Lock, Loader2, Pencil, Trash2 } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { useAuth } from "../../contexts/AuthContext";
import { Comment, createComment, deleteComment, fetchComments, updateComment } from "../../lib/api";

export function CommentSection({ postId }: { postId: string | number }) {
  const { user, login } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [newContent, setNewContent] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState("");
  const [isMutating, setIsMutating] = useState(false);
  const [error, setError] = useState("");

  const loadComments = async () => {
    setIsLoading(true);
    try {
      setComments(await fetchComments(postId));
    } catch (err) {
      setError("댓글을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadComments();
  }, [postId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !newContent.trim() || isSubmitting) return;

    setIsSubmitting(true);
    setError("");
    try {
      await createComment(postId, newContent);
      setNewContent("");
      await loadComments();
    } catch (err) {
      setError("댓글 작성에 실패했습니다. GitHub 로그인이 필요할 수 있습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const startEditing = (comment: Comment) => {
    setEditingId(comment.id);
    setEditingContent(comment.content);
    setError("");
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingId === null || !editingContent.trim() || isMutating) return;

    setIsMutating(true);
    setError("");
    try {
      await updateComment(postId, editingId, editingContent);
      setEditingId(null);
      setEditingContent("");
      await loadComments();
    } catch (err) {
      setError("댓글 수정에 실패했습니다.");
    } finally {
      setIsMutating(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    if (isMutating || !window.confirm("댓글을 삭제할까요?")) return;

    setIsMutating(true);
    setError("");
    try {
      await deleteComment(postId, commentId);
      if (editingId === commentId) {
        setEditingId(null);
        setEditingContent("");
      }
      await loadComments();
    } catch (err) {
      setError("댓글 삭제에 실패했습니다.");
    } finally {
      setIsMutating(false);
    }
  };

  return (
    <div className="mt-20 border-t border-slate-100 pt-16">
      <div className="flex items-center justify-between mb-10">
        <h3 className="text-2xl font-bold text-slate-900 flex items-center">
          <MessageCircle className="w-6 h-6 mr-3 text-blue-600" />
          Comments <span className="ml-3 text-slate-300 text-lg font-normal">({comments.length})</span>
        </h3>
      </div>

      {user ? (
        <form onSubmit={handleSubmit} className="mb-12 bg-white rounded-3xl border border-slate-200 p-6 md:p-8 shadow-sm">
          <div className="flex items-start space-x-4 mb-6">
            {user.avatar ? (
              <img
                src={user.avatar}
                className="w-12 h-12 rounded-2xl border border-slate-100 object-cover"
                alt="avatar"
              />
            ) : (
              <div className="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-600 font-bold">
                {user.name?.charAt(0) || "U"}
              </div>
            )}
            <div className="flex-1 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-900">{user.name}</span>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none">Logged in via GitHub</span>
              </div>
              <textarea
                value={newContent}
                onChange={e => setNewContent(e.target.value)}
                placeholder="댓글을 남겨보세요..."
                required
                rows={3}
                maxLength={500}
                className="w-full px-4 py-3 rounded-2xl bg-slate-50 border border-transparent focus:bg-white focus:border-blue-500 outline-none transition-all font-medium text-sm resize-none"
              />
            </div>
          </div>
          {error && <p className="mb-4 text-sm font-bold text-red-500">{error}</p>}
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center space-x-2 px-8 py-3 bg-blue-600 text-white rounded-2xl font-bold hover:bg-blue-700 transition-all disabled:opacity-50 shadow-lg shadow-blue-200"
            >
              {isSubmitting ? "Posting..." : (
                <>
                  <span>Post Comment</span>
                  <Send className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      ) : (
        <div className="mb-12 p-8 rounded-[32px] bg-slate-50 border border-dashed border-slate-200 text-center">
          <Lock className="w-8 h-8 text-slate-300 mx-auto mb-4" />
          <p className="text-slate-500 font-medium mb-6">댓글을 작성하려면 깃허브 로그인이 필요합니다.</p>
          <button
            onClick={login}
            className="px-8 py-3 bg-slate-900 text-white rounded-2xl font-bold hover:scale-105 transition-transform flex items-center mx-auto"
          >
            <Github className="w-5 h-5 mr-3" /> 깃허브로 로그인하기
          </button>
        </div>
      )}

      {isLoading ? (
        <div className="py-12 flex justify-center text-slate-300">
          <Loader2 className="w-6 h-6 animate-spin" />
        </div>
      ) : (
        <div className="space-y-6">
          <AnimatePresence initial={false}>
            {comments.map((comment) => (
              <motion.div
                id={`comment-${comment.id}`}
                key={comment.id}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-white rounded-3xl border border-slate-100 p-6 shadow-sm"
              >
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center space-x-3">
                    {comment.authorAvatar ? (
                      <img src={comment.authorAvatar} className="w-10 h-10 rounded-2xl object-cover" alt="avatar" />
                    ) : (
                      <div className="w-10 h-10 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-600 font-bold">
                        {comment.authorName.charAt(0).toUpperCase()}
                      </div>
                    )}
                    <a
                      href={comment.authorGithubUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="font-bold text-slate-900 hover:text-blue-600 transition-colors flex items-center"
                    >
                      {comment.authorName}
                      <Github className="w-3 h-3 ml-1.5 opacity-30" />
                    </a>
                  </div>
                  <div className="flex items-center space-x-3">
                    {comment.ownedByCurrentUser && (
                      <>
                        <button
                          type="button"
                          onClick={() => startEditing(comment)}
                          disabled={isMutating}
                          className="inline-flex items-center text-xs font-bold text-slate-400 hover:text-blue-600 disabled:opacity-50"
                        >
                          <Pencil className="w-3 h-3 mr-1" />
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(comment.id)}
                          disabled={isMutating}
                          className="inline-flex items-center text-xs font-bold text-slate-400 hover:text-red-500 disabled:opacity-50"
                        >
                          <Trash2 className="w-3 h-3 mr-1" />
                          삭제
                        </button>
                      </>
                    )}
                    <span className="text-[10px] text-slate-400 font-bold uppercase">
                      {format(new Date(comment.date), "MMM d, yyyy HH:mm")}
                    </span>
                  </div>
                </div>
                {editingId === comment.id ? (
                  <form onSubmit={handleUpdate} className="md:pl-13">
                    <textarea
                      value={editingContent}
                      onChange={e => setEditingContent(e.target.value)}
                      required
                      rows={3}
                      maxLength={500}
                      className="w-full px-4 py-3 rounded-2xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-500 outline-none transition-all font-medium text-sm resize-none"
                    />
                    <div className="mt-3 flex justify-end space-x-2">
                      <button
                        type="button"
                        disabled={isMutating}
                        onClick={() => setEditingId(null)}
                        className="px-4 py-2 text-sm font-bold text-slate-500 hover:text-slate-700 disabled:opacity-50"
                      >
                        취소
                      </button>
                      <button
                        type="submit"
                        disabled={isMutating || !editingContent.trim()}
                        className="px-4 py-2 rounded-xl bg-blue-600 text-sm font-bold text-white hover:bg-blue-700 disabled:opacity-50"
                      >
                        저장
                      </button>
                    </div>
                  </form>
                ) : (
                  <p className="text-slate-700 leading-relaxed md:pl-13 whitespace-pre-wrap">
                    {comment.content}
                  </p>
                )}
              </motion.div>
            ))}
            {comments.length === 0 && (
              <div className="text-center py-12 text-slate-400">
                <p className="font-medium italic">첫 번째 댓글을 남겨보세요.</p>
              </div>
            )}
          </AnimatePresence>
        </div>
      )}
    </div>
  );
}
