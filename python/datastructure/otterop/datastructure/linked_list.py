from otterop.lang.panic import Panic as _Panic
from otterop.lang.pure_iterator import PureIterator as _PureIterator
from otterop.lang.string import String as _String
from otterop.lang.oop_object import OOPObject as _OOPObject
from otterop.datastructure.linked_list_node import LinkedListNode as _LinkedListNode
from otterop.datastructure._linked_list_iterator import LinkedListIterator as _LinkedListIterator

class LinkedList:
            
    def __init__(self):
        self._head = None
        self._tail = None
        self._size = 0

    def add_before(self, node, value):
        new_node = _LinkedListNode(value)
        new_node._set_list(self)
        self.add_node_before(node, new_node)
        return new_node

    def _node_of_different_list(self):
        _Panic.invalid_operation(_String.wrap("node of different list"))

    def _remove_on_empty_list(self):
        _Panic.invalid_operation(_String.wrap("remove called on empty list"))

    def add_node_before(self, node, new_node):
        if node.list() != new_node.list() or node.list() != self:
            self._node_of_different_list()
        prev_node = node.prev()
        if prev_node == None:
            new_node.list()._head = new_node
        else:
            prev_node._set_next(new_node)
        new_node._set_prev(prev_node)
        new_node._set_next(node)
        node._set_prev(new_node)
        self._size += 1

    def add_after(self, node, value):
        new_node = _LinkedListNode(value)
        new_node._set_list(self)
        self.add_node_after(node, new_node)
        return new_node

    def add_node_after(self, node, new_node):
        if node.list() != new_node.list() or node.list() != self:
            self._node_of_different_list()
        next_node = node.next()
        if next_node == None:
            new_node.list()._tail = new_node
        else:
            next_node._set_prev(new_node)
        new_node._set_next(next_node)
        new_node._set_prev(node)
        node._set_next(new_node)
        self._size += 1

    def add_first(self, value):
        new_node = _LinkedListNode(value)
        new_node._set_list(self)
        self.add_node_first(new_node)
        return new_node

    def add_node_first(self, new_node):
        if self._head == None:
            if new_node.list() != self:
                self._node_of_different_list()
            self._head = new_node
            self._tail = new_node
            self._size += 1
        else:
            self.add_node_before(self._head, new_node)

    def add_last(self, value):
        new_node = _LinkedListNode(value)
        new_node._set_list(self)
        self.add_node_last(new_node)
        return new_node

    def add_node_last(self, new_node):
        if self._tail == None:
            if new_node.list() != self:
                self._node_of_different_list()
            self._head = new_node
            self._tail = new_node
            self._size += 1
        else:
            self.add_node_after(self._tail, new_node)

    def clear(self):
        self._head = None
        self._tail = None
        self._size = 0

    def remove_first(self):
        if self._head != None:
            self.remove_node(self._head)
        else:
            self._remove_on_empty_list()

    def remove_last(self):
        if self._tail != None:
            self.remove_node(self._tail)
        else:
            self._remove_on_empty_list()

    def remove(self, value):
        curr = self._head
        while curr != None:
            if OOPObject._is(curr.value(), value):
                self.remove_node(curr)
                return True
            curr = curr.next()
        return False

    def remove_node(self, node):
        if node.list() != self:
            _Panic.invalid_operation(_String.wrap("node of different list"))
        prev = node.prev()
        next = node.next()
        if prev != None:
            prev._set_next(next)
        else:
            node.list()._head = next
        if next != None:
            next._set_prev(prev)
        else:
            node.list()._tail = prev
        node._set_prev(None)
        node._set_next(None)
        self._size -= 1

    def size(self):
        return self._size

    def first(self):
        return self._head

    def last(self):
        return self._tail

    def oop_iterator(self):
        return _LinkedListIterator(self)

    def __iter__(self):
        return _PureIterator.new_iterator(self.oop_iterator())
