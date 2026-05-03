# Unit Testing Rules

## 1. Framework setup

- Annotate every test class with `@ExtendWith(MockitoExtension.class)`. No manual stub classes.
- Use **AssertJ** for all assertions: import `assertThat` and `assertThatThrownBy` from `org.assertj.core.api.Assertions`. Never use JUnit's `assertEquals`, `assertTrue`, `assertThrows`, etc.
- Use **JUnit Jupiter** as the test runner.
- Write unit tests only — no integration or end-to-end tests.

## 2. Class structure

- The variable holding the class under test must always be named **`unit`**.
- Use `@InjectMocks private <ClassName> unit;` wherever Mockito can reliably wire the constructor (i.e., when there is exactly one constructor and all its parameters are mockable types). This is the preferred form.
- When the production class has non-injectable constructor parameters (primitives, `String`, `Path`, etc.), or when ambiguity between constructors would cause Mockito to pick the wrong one, declare `unit` as a **local variable inside `// Given`** — do **not** use `@BeforeEach` for this.
- Declare mocks of generic types (e.g., `HttpResponse<Path>`) as `@Mock` fields on the class, not as inline `mock(...)` calls inside test methods.
- No `@BeforeEach` setUp helpers, no private helper methods, no shared constants. Every test must be **self-contained** — inline everything.
- No `@Spy`. No `var` — always write explicit types.

## 3. Test method structure

- Name pattern: **`should_<expectation>_when_<condition>`**.
- Split the test body with `// Given`, `// When`, `// Then` comment blocks.
- **Omit `// Given` entirely** when there is nothing to declare in it. Start directly with `// When`. Never write `// Given / When` or `// Given (no setup needed)`.
- Cover the **happy path and one main alternative path** only. No exhaustive branch coverage.

## 4. Assertion and verification style

- **Chain `assertThat()` calls** when the subject is the same object:
  ```java
  assertThat(result).isEqualTo(expected).exists();   // good
  assertThat(result).isEqualTo(expected);             // bad — separate call
  assertThat(result).exists();                        //       on same subject
  ```
- Use AssertJ's dedicated predicates — never wrap a boolean in `assertThat(...).isTrue()`:
  ```java
  assertThat(str).startsWith("foo");              // good
  assertThat(str.startsWith("foo")).isTrue();     // bad (Sonar S5785)
  ```
- **Avoid `ArgumentCaptor`** unless it is the only practical way to assert on a complex argument.
- When `ArgumentCaptor` is needed and the type parameter would cause an unchecked warning, use **`ArgumentCaptor.captor()`** instead of `ArgumentCaptor.forClass(...)`. No `@SuppressWarnings("unchecked")` should be needed.
- Write `verify(mock).method(...)` — the default count is 1, never add `times(1)` explicitly.
- Avoid `verifyNoMoreInteractions` — it makes tests brittle to unrelated refactors.
