# Lessons

(When we apply fixes, append short lessons here.)

2026-05-05 | Wrong parameter type used for LinearProgressIndicator; passed a lambda where a Float was expected (or used deprecated overload). Fixed by passing the Float produced by animateFloatAsState: `LinearProgressIndicator(progress = progress, ...)`. Lesson: verify Compose API parameter types and prefer the currently recommended overloads; check compiler warnings for deprecation guidance.


