import re

with open("website/app/dashboard/page.tsx", "r") as f:
    content = f.read()

# We need to get the fcmToken from activeDb / users / currentUser.uid
# There is a snapshot that sets `deviceLinked`. Can we just fetch the fcmToken doc here?
