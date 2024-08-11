package lang

type wrapperOOPIterable[FROM any, TO any] struct {
	slice []FROM
	wrap func(FROM) TO
	to TO
}

func (this wrapperOOPIterable[FROM,TO]) OOPIterator() OOPIterator[TO] {
	return WrapperOOPIteratorWrapSlice(this.slice, this.wrap, this.to)
}


func WrapperOOPIterableWrapSlice[OOP any, PURE any](slice []PURE, wrap func(PURE) OOP, oopClass OOP) OOPIterable[OOP] {
	ret := wrapperOOPIterable[PURE, OOP] {
		slice,
		wrap,
		oopClass,
	}
	return ret
}

func WrapperOOPIterableUnwrapSlice[OOP any, PURE any](slice OOPIterable[OOP], unwrap func(OOP) PURE, pureClass PURE) []PURE {
	return WrapperOOPIteratorUnwrapSlice(slice.OOPIterator(), unwrap, pureClass)
}
