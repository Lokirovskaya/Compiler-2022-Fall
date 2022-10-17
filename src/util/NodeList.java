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
        head.insertNextSelf(e);
    }

    public void addLast(E e) {
        tail.insertPrevSelf(e);
    }

    public void forEach(Consumer<NodeListNode<E>> func) {
        // 对 delete 安全，允许一边遍历一遍删除
        // 用户操作的节点，实际上是 node.next
        for (NodeListNode<E> node = this.head; node != this.tail.prev; ) {
            func.accept(node);
            if (node.nextDeleted) node.nextDeleted = false;
            else node = node.next;
        }
    }

    public static void main(String[] args) {
        NodeList<Integer> list = new NodeList<>();
        list.addLast(1);
        list.addLast(1);
        list.addLast(3);
        list.addLast(4);
        list.addLast(5);
        list.addFirst(0);

        list.forEach(p -> {
            if ( p.get(1) != null && p.get(1) == 1) {
                p.delete(1);

            }
        });
        list.forEach(p -> System.out.println(p.get()));
    }
}
