package lang

func PanicIndexOutOfBounds(message *String)  {
    panic(message.Unwrap())
}

func PanicInvalidOperation(message *String)  {
    panic(message.Unwrap())
}
