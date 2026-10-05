# LumenLMS v10.7.22 Changes

## Downloads page
- Added a Downloads page to every role's sidebar, positioned above Logout.
- Shows the signed-in user's download history with title, file name, category, and timestamp.
- Added Clear History action.
- Assignment submission downloads and course-material downloads are recorded automatically.
- Download history table is created automatically if missing.

## Instructor lesson attachments
- Instructor Add/Edit Lesson dialog now has an Add Attachment button.
- Supports PDF, DOC, DOCX, TXT, PPT and PPTX lesson notes/resources.
- Files are copied into `%USERPROFILE%/LumenLMS/uploads/materials` on Windows (via user.home) and stored as FILE materials.
- Students see FILE materials in Resources & Notes with a one-click Download button.
- Downloaded course materials are recorded in the Downloads page.

## Database
- Added `database/migrate_download_history.sql`.
- Added `download_history` table to `database/lms.sql`.
