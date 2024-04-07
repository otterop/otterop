package datastructure

import (
    lang "github.com/otterop/otterop/go/lang"
)


type LinkedList[T any] struct {
    head *LinkedListNode[T]
    tail *LinkedListNode[T]
    size int
}




func LinkedListNew[T any]() *LinkedList[T] {
    this := new(LinkedList[T])
    this.head = nil
    this.tail = nil
    this.size = 0
    var t *testInternal = testInternalNew()
    t.testMethod()
    testInternalTestMethod2()
    return this
}

func (this *LinkedList[T]) AddBefore(node *LinkedListNode[T], value T) *LinkedListNode[T] {
    var newNode *LinkedListNode[T] = LinkedListNodeNew[T](value)
    newNode.setList(this)
    this.AddNodeBefore(node, newNode)
    return newNode
}

func (this *LinkedList[T]) nodeOfDifferentList()  {
    lang.PanicInvalidOperation(lang.StringWrap(lang.StringLiteral("node of different list")))
}

func (this *LinkedList[T]) removeOnEmptyList()  {
    lang.PanicInvalidOperation(lang.StringWrap(lang.StringLiteral("remove called on empty list")))
}

func (this *LinkedList[T]) AddNodeBefore(node *LinkedListNode[T], newNode *LinkedListNode[T])  {
    
    if node.List() != newNode.List() || node.List() != this {
        this.nodeOfDifferentList()
    }

    var prevNode *LinkedListNode[T] = node.Prev()
    
    if prevNode == nil {
        newNode.List().head = newNode
    }

    newNode.setPrev(prevNode)
    newNode.setNext(node)
    prevNode.setNext(newNode)
    node.setPrev(newNode)
    this.size++
}

func (this *LinkedList[T]) AddAfter(node *LinkedListNode[T], value T) *LinkedListNode[T] {
    var newNode *LinkedListNode[T] = LinkedListNodeNew[T](value)
    newNode.setList(this)
    this.AddNodeAfter(node, newNode)
    return newNode
}

func (this *LinkedList[T]) AddNodeAfter(node *LinkedListNode[T], newNode *LinkedListNode[T])  {
    
    if node.List() != newNode.List() || node.List() != this {
        this.nodeOfDifferentList()
    }

    var nextNode *LinkedListNode[T] = node.Next()
    
    if nextNode == nil {
        newNode.List().tail = newNode
    }

    newNode.setNext(nextNode)
    newNode.setPrev(node)
    nextNode.setPrev(newNode)
    node.setNext(newNode)
    this.size++
}

func (this *LinkedList[T]) AddFirst(value T) *LinkedListNode[T] {
    var newNode *LinkedListNode[T] = LinkedListNodeNew[T](value)
    newNode.setList(this)
    this.AddNodeFirst(newNode)
    return newNode
}

func (this *LinkedList[T]) AddNodeFirst(newNode *LinkedListNode[T])  {
    
    if this.head == nil {
        
        if newNode.List() != this {
            this.nodeOfDifferentList()
        }

        this.head = newNode
        this.tail = newNode
    } else  {
        this.AddNodeBefore(this.head, newNode)
    }
}

func (this *LinkedList[T]) AddLast(value T) *LinkedListNode[T] {
    var newNode *LinkedListNode[T] = LinkedListNodeNew[T](value)
    newNode.setList(this)
    this.AddNodeLast(newNode)
    return newNode
}

func (this *LinkedList[T]) AddNodeLast(newNode *LinkedListNode[T])  {
    
    if this.tail == nil {
        
        if newNode.List() != this {
            this.nodeOfDifferentList()
        }

        this.head = newNode
        this.tail = newNode
    } else  {
        this.AddNodeAfter(this.tail, newNode)
    }
}

func (this *LinkedList[T]) Clear()  {
    this.head = nil
    this.tail = nil
    this.size = 0
}

func (this *LinkedList[T]) RemoveFirst()  {
    
    if this.head != nil {
        this.RemoveNode(this.head)
    } else  {
        this.removeOnEmptyList()
    }
}

func (this *LinkedList[T]) RemoveLast()  {
    
    if this.tail != nil {
        this.RemoveNode(this.tail)
    } else  {
        this.removeOnEmptyList()
    }
}

func (this *LinkedList[T]) Remove(value T) bool {
    var curr *LinkedListNode[T] = this.head
    
    for curr != nil {
        
        if lang.OOPObjectIs(curr.Value(), value) {
            this.RemoveNode(curr)
            return true
        }
        curr = curr.Next()
    }
    return false
}

func (this *LinkedList[T]) RemoveNode(node *LinkedListNode[T])  {
    
    if node.List() != this {
        lang.PanicInvalidOperation(lang.StringWrap(lang.StringLiteral("node of different list")))
    }

    var prev *LinkedListNode[T] = node.Prev()
    var next *LinkedListNode[T] = node.Next()
    
    if prev != nil {
        prev.setNext(next)
    } else  {
        node.List().head = next
    }
    
    if next != nil {
        next.setPrev(prev)
    } else  {
        node.List().tail = prev
    }
    node.setPrev(nil)
    node.setNext(nil)
    this.size--
}

func (this *LinkedList[T]) Size() int {
    return this.size
}