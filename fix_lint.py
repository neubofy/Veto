with open('website/app/page.tsx', 'r') as f:
    content = f.read()

# Fix unescaped entities
content = content.replace("driver's", "driver&apos;s")
content = content.replace("don't", "don&apos;t")
content = content.replace("Don't", "Don&apos;t")

with open('website/app/page.tsx', 'w') as f:
    f.write(content)
