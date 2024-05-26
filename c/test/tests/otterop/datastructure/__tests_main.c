#include "unity_fixture.h"

static void __run_all_tests(void) {
    RUN_TEST_GROUP(otterop_datastructure_TestLinkedList);
    RUN_TEST_GROUP(otterop_datastructure_TestList);
}

int main(int argc, const char *argv[]) {
    return UnityMain(argc, argv, __run_all_tests);
}
