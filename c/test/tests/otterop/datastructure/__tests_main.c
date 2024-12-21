#include "unity_fixture.h"

#include "__tests_main_manual.c"

static void __run_all_tests(void) {
    RUN_TEST_GROUP(otterop_datastructure_TestLinkedList);
    RUN_TEST_GROUP(otterop_datastructure_TestList);
    RUN_TEST_GROUP(otterop_datastructure_TestStringBuffer);
    __run_all_tests_manual();
}

int main(int argc, const char *argv[]) {
    return UnityMain(argc, argv, __run_all_tests);
}
