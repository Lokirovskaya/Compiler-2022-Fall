package util;

// 允许在循环中删除节点的链表
// 循环中删除下一节点以及之后的节点是安全的，因此提供给用户的是下一节点的信息
// 保证 this.next 不为空，详见 forEach 的实现
public class NodeListNode<E> {
    private E data;
    NodeListNode<E> prev, next;
    // 下一个节点是否被删除
    // 如果是，则在 forEach 进入下一次循环时，不要移动 node，避免漏掉一个节点
    boolean nextDeleted = false;

    public E get() {
        return this.next.getSelf();
    }

    public void set(E e) {
        this.next.data = e;
    }

    public E get(int offset) {
        if (offset == 0) return this.get();
        NodeListNode<E> node = this.next.nodeOffset(offset);
        return node == null ? null : node.getSelf();
    }

    public void insertNext(E e) {
        this.next.insertNextSelf(e);
    }

    public void insertPrev(E e) {
        this.next.insertPrevSelf(e);
    }

    public void delete() {
        nextDeleted = true;
        this.next.deleteSelf();
    }

    public void delete(int offset) {
        assert offset >= 0;
        nextDeleted = true;
        if (offset == 0) this.delete();
        NodeListNode<E> node = this.next.nodeOffset(offset);
        if (node != null) node.deleteSelf();
    }

    // 以下方法对用户隐藏 //

    E getSelf() {
        return data;
    }

    void insertNextSelf(E e) {
        NodeListNode<E> p = new NodeListNode<>();
        p.data = e;
        p.prev = this;
        p.next = this.next;
        this.next = p;
        p.next.prev = p;
    }

    void insertPrevSelf(E e) {
        this.prev.insertNextSelf(e);
    }

    void deleteSelf() {
        this.prev.next = this.next;
        this.next.prev = this.prev;
        this.prev = this.next = null;
        this.data = null;
    }

    private NodeListNode<E> nodeOffset(int offset) {
        if (offset == 0) return this;
        else {
            NodeListNode<E> n = this;
            for (int i = 0; i < offset; i++) {
                if (n.next == null) return null;
                n = n.next;
            }
            return n;
        }
    }
}
