#include <otterop/datastructure/int/test_internal.h>
#include <gc.h>

typedef struct otterop_datastructure_TestInternal_s otterop_datastructure_TestInternal_t;

typedef struct otterop_datastructure_TestInternal_s {
} otterop_datastructure_TestInternal_t;




void otterop_datastructure_TestInternal_test_method(otterop_datastructure_TestInternal_t *this) {
}

void otterop_datastructure_TestInternal_test_method2() {
}

otterop_datastructure_TestInternal_t* otterop_datastructure_TestInternal_new() {
    otterop_datastructure_TestInternal_t *this = GC_malloc(sizeof(*this));
    return this;
}
