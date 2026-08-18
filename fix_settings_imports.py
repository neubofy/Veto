import re

with open("app/src/main/java/com/neubofy/veto/ui/settings/SettingsActivity.kt", "r") as f:
    content = f.read()

content = content.replace("import com.neubofy.veto.ui.common.PasswordSetDialog\n", "import com.google.firebase.auth.FirebaseAuth\nimport com.neubofy.veto.ui.common.PasswordSetDialog\n")

with open("app/src/main/java/com/neubofy/veto/ui/settings/SettingsActivity.kt", "w") as f:
    f.write(content)
