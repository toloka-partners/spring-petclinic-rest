import re

# Read the OpenAPI file
with open('src/main/resources/openapi.yml', 'r') as f:
    content = f.read()

# Add weight to Pet schema after birth_date
pet_pattern = r'(birth_date:\s*\n\s+type: string\s*\n\s+format: date\s*\n\s+description: The pet'"'"'s birth date\s*\n\s+example: "2023-01-15")'
pet_replacement = r'\1\n    weight:\n      type: number\n      format: double\n      nullable: true\n      description: Pet weight in kg'

content = re.sub(pet_pattern, pet_replacement, content)

# Add weight to PetFieldsDto schema after birthDate  
petfields_pattern = r'(birthDate:\s*\n\s+type: string\s*\n\s+format: date\s*\n\s+description: The pet'"'"'s birth date\s*\n\s+example: "2023-01-15")'
petfields_replacement = r'\1\n    weight:\n      type: number\n      format: double\n      nullable: true\n      description: Pet weight in kg'

content = re.sub(petfields_pattern, petfields_replacement, content)

# Write back
with open('src/main/resources/openapi.yml', 'w') as f:
    f.write(content)

print("OpenAPI updated successfully")
