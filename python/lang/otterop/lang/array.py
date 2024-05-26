#
# Copyright (c) 2023 The OtterOP Authors. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
#    * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above
# copyright notice, this list of conditions and the following disclaimer
# in the documentation and/or other materials provided with the
# distribution.
#    * Neither the name of the copyright holder nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#


from otterop.lang.string import String as _String
from otterop.lang.panic import Panic as _Panic
from otterop.lang.pure_iterator import PureIterator as _PureIterator
from otterop.lang._array_iterator import ArrayIterator as _ArrayIterator

class Array:
    def __init__(self, list, start, end):
        self._wrapped = list
        self._start = start
        self._end = end

    def get(self, i):
        return self._wrapped[self._start + i]

    def set(self, i, value):
        self._wrapped[self._start + i] = value

    def size(self):
        return self._end - self._start

    def slice(self, start, end):
        new_start = self._start + start
        new_end = self._start + end
        if new_start < self._start or new_start > self._end or new_end < new_start \
           or new_end > self._end:
             raise IndexError("slice arguments out of bounds")
        return Array(self._wrapped, new_start, new_end)

    def unwrap(self):
        return self._wrapped

    def oop_iterator(self):
        return _ArrayIterator(self)

    def __iter__(self):
        return _PureIterator.new_iterator(self.oop_iterator())

    @staticmethod
    def new_array(size, clazz):
        return Array([None] * size, 0, size)

    @staticmethod
    def copy(src, src_pos, dst, dst_pos, length):
        if src._start + src_pos + length > src._end:
            _Panic.index_out_of_bounds("source index out of bounds")
        if dst._start + dst_pos + length > dst._end:
            _Panic.index_out_of_bounds("destination index out of bounds")
        dst._wrapped[dst._start + dst_pos:dst._start + dst_pos + length] = src._wrapped[src._start + src_pos:src._start + src_pos + length]

    @staticmethod
    def wrap(list):
        return Array(list, 0, len(list))

    @staticmethod
    def wrap_string(list):
        list = [ _String.wrap(s) for s in list ]
        return Array.wrap(list)
