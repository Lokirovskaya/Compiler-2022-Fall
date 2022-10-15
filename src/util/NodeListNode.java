package util;

public class NodeListNode<E> {
    private E data;
    NodeListNode<E> prev, next;

    public E get(int offset) {
        if (offset == 0) return data;
        NodeListNode<E> node = this.nodeOffset(offset);
        return node == null ? null : node.data;
    }

    private NodeListNode<E> nodeOffset(int offset) {
        NodeListNode<E> n = this;
        if (offset == 0) return this;
        else if (offset > 0) {
            for (int i = 0; i < offset; i++) {
                if (n.next == null) return null;
                n = n.next;
            }
        }
        else {
            for (int i = 0; i < -offset; i++) {
                if (n.prev == null) return null;
                n = n.prev;
            }
        }
        return n;
    }

    public void insertNext(E e) {
        NodeListNode<E> p = new NodeListNode<>();
        p.data = e;
        p.prev = this;
        p.next = this.next;
        this.next = p;
        p.next.prev = p;
    }

    public void insertPrev(E e) {
        this.prev.insertNext(e);
    }

    // 在 forEach 循环中删除 this 是危险的，循环中只允许调用 deleteNext 或 deletePrev
    public void delete() {
        this.prev.next = this.next;
        this.next.prev = this.prev;
        this.prev = this.next = null;
        this.data = null;
    }

    public void deleteNext() {
        this.next.delete();
    }

    public void deletePrev() {
        this.prev.delete();
    }

    public void replace(int offset, E e) {
        NodeListNode<E> node = this.nodeOffset(offset);
        if (node != null) node.data = e;
    }
}
