package util;

import java.util.function.Consumer;

public class NodeList<E> {
    private final NodeListNode<E> head, tail;

    public NodeList() {
        head = new NodeListNode<>();
        tail = new NodeListNode<>();
        head.next = tail;
        tail.prev = head;
    }

    public void addFirst(E e) {
        head.insertNext(e);
    }

    public void addLast(E e) {
        tail.insertPrev(e);
    }

    public void forEach(Consumer<NodeListNode<E>> func) {
        // 请保证 node.next 可访问，因此禁止调用 node.delete
        for (NodeListNode<E> node = this.head.next; node != this.tail; node = node.next) {
            func.accept(node);
        }
    }
}
