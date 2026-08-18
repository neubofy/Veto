import re

with open("app/src/main/java/com/neubofy/veto/ui/settings/SettingsActivity.kt", "r") as f:
    content = f.read()

content = content.replace("import java.net.URL\n", "")
content = content.replace("import java.net.HttpURLConnection\n", "")
content = content.replace("import org.json.JSONObject\n", "")
content = content.replace("import com.google.firebase.auth.FirebaseAuth\n", "")

with open("app/src/main/java/com/neubofy/veto/ui/settings/SettingsActivity.kt", "w") as f:
    f.write(content)
