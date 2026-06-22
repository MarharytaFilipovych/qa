## My thoughts about AI-generated tests

### What I used

I used **claude** for those purposes.
_My thoughts are not straightforward. On one side, it did generate lots of tests. On another side, some of their quality is not the highest._

### Advantages

1) Relatively fast (at least definitely faster than doing it manually)
2) Most of them work, so the only thing you need to do is to adjust those that don't work and maybe clean some things.
3) Time-saver

### Disadvantages

1) To get full coverage (or something close to full) I had to ask claude 3 times, even though I stated my need in the first prompt.
2) Some tests do not work due to various reasons:
   * unnecessary stubbings `UnnecessaryStubbingException`
   * misconfigurations: `NotificationServiceApplicationTest`, `WorkflowServiceApplicationTest` and `UserServiceApplicationTest` failed to load the application context
   * _wanted but not invoked_: AI failed to understand where the method should have been invoked
   * assertion errors (not very common): occurred within `objectMapper_serializesLocalDateTime`, `isValid_returnsFalse_whenTooYoung`, `isValid_returnsFalse_whenFutureDate` -> incorrect assertion
3) extensive love to `any()` arguments 
4) no love for constants
5) common usage of deprecated annotations: `@MockBean` instead of `@Mock`
6) strange comments instead of `//Arrange //Act //Assert` or `//Given //When //Then` (even though I did not ask to put those, but still)

### Conclusion

Although, my list of disadvantages is bigger than that of advantages, I can confidently state that you should use AI for test generation because it's definitely a time-saver.
However, you have to define a clear test generation strategy (prompt) and always carefully review and adjust the outcome.