package lang

func PanicNew() *Panic {
    this := new(Panic)
    return this
}

type Panic struct {
}




func PanicIndexOutOfBounds(message *String)  {
    panic(message.Unwrap())
}
