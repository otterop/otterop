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

from io import StringIO as _StringIO
class String:

    def __init__(self, _wrapped):
        self._wrapped = _wrapped

    def length(self):
        return len(self._wrapped)

    def __str__(self):
        return self._wrapped

    def compare_to(self, other):
        if not isinstance(other, String):
            return 1
        if self._wrapped < other._wrapped:
            return -1
        elif self._wrapped > other._wrapped:
            return 1
        return 0

    def unwrap(self):
        return self._wrapped

    @staticmethod
    def wrap(_wrapped):
        return String(_wrapped)

    @staticmethod
    def unwrap(_wrapped):
        return _wrapped._wrapped

    @staticmethod
    def concat(strings):
        it = strings.oop_iterator()
        sb = _StringIO()
        while it.has_next():
            s = it.next()
            sb.write(String.unwrap(s))
        return String.wrap(sb.getvalue())
