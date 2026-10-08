-- Run this ONCE against an existing database that was created from an older LumenLMS seed file.
-- It removes only the bundled demo courses/accounts. It does NOT remove courses created by you unless
-- they use one of the exact demo course titles below.

DELETE FROM courses
WHERE title IN (
  'Java Programming Masterclass',
  'Full-Stack Web Development',
  'Data Science with Python',
  'Introduction to Machine Learning'
);

DELETE FROM users
WHERE email IN (
  'sarah.instructor@lms.com',
  'david.instructor@lms.com',
  'emily.student@lms.com',
  'michael.student@lms.com',
  'priya.student@lms.com'
);
