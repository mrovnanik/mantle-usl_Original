List<String> tagField = new ArrayList<String>()

//return empty array if null
if (manualCategory == null) {
    ec.context.put("tagField", tagField)
    return
}

if (manualCategory.toString().startsWith('[') && manualCategory.toString().endsWith(']')) {
    tagField.push(manualCategory.substring(1, manualCategory.length()-1))
} else {
    tagField.push(manualCategory)
}

ec.context.put("tagField", tagField)