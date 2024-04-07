package lang

func OOPObjectIs[T any](a T, b T) bool {
    return (any)(a) == (any)(b)
}
