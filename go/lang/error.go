package lang



type Error struct {
    code int
    message *String
}




func ErrorNew(code int, message *String) *Error {
    this := new(Error)
    this.code = code
    this.message = message
    return this
}

func (this *Error) Code() int {
    return this.code
}

func (this *Error) Message() *String {
    return this.message
}