# Will assume it is has one the following forms:
# 1. Classname
# 2. Classname<Some separator>Method<Ignored>
split_index = -1
for i, c in enumerate(tag):
    if not c.isalnum():
        split_index = i
        break

if split_index == -1:
    cls = tag
else:
    cls = tag[:split_index]
    method = ""
    for c in tag[split_index + 1:]:
        if not c.isalnum():
            break
        method += c
    if not method:
        del method