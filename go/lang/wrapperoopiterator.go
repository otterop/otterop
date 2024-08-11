package lang

type wrapperOOPIteratorSlice[FROM any, TO any] struct {
	idx int
	slice []FROM
	convert func(FROM) TO
}

func wrapperOOPIteratorSliceNew[FROM any, TO any](slice []FROM, convert func(FROM) TO) OOPIterator[TO] {
	ret := wrapperOOPIteratorSlice[FROM,TO] {
		-1,
		slice,
		convert,
	}
	return &ret
}

func (this *wrapperOOPIteratorSlice[FROM,TO]) HasNext() bool {
	return this.idx < len(this.slice) - 1
}

func (this *wrapperOOPIteratorSlice[FROM,TO]) Next() TO {
	this.idx++
	from := this.slice[this.idx]
	if this.convert == nil {
		to := any(from).(TO)
		return to
	}
	return this.convert(from)
}


func WrapperOOPIteratorWrapSlice[OOP any, PURE any](it []PURE, wrap func(PURE) OOP, oopClass OOP) OOPIterator[OOP] {
	return wrapperOOPIteratorSliceNew(it, wrap)
}

func WrapperOOPIteratorUnwrapSlice[OOP any, PURE any](it OOPIterator[OOP], convert func(OOP) PURE, pureClass PURE) []PURE {
	ret := make([]PURE, 0)
	for it.HasNext() {
		next := it.Next()
		if convert == nil {
			to := any(next).(PURE)
			ret = append(ret, to)
		} else {
			ret = append(ret, convert(next))
		}
	}
	return ret
}
