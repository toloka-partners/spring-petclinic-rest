import yaml
import sys

# Read the OpenAPI file
with open('src/main/resources/openapi.yml', 'r') as file:
    openapi = yaml.safe_load(file)

# Add weight field to Pet schema
if 'components' in openapi and 'schemas' in openapi['components']:
    if 'Pet' in openapi['components']['schemas']:
        pet_schema = openapi['components']['schemas']['Pet']
        if 'properties' in pet_schema:
            # Insert weight after birth_date
            new_props = {}
            for key, value in pet_schema['properties'].items():
                new_props[key] = value
                if key == 'birth_date':
                    new_props['weight'] = {
                        'type': 'number',
                        'format': 'double', 
                        'nullable': True,
                        'description': 'Pet weight in kg'
                    }
            pet_schema['properties'] = new_props
    
    # Add weight field to PetFieldsDto schema
    if 'PetFieldsDto' in openapi['components']['schemas']:
        pet_fields_schema = openapi['components']['schemas']['PetFieldsDto']
        if 'properties' in pet_fields_schema:
            new_props = {}
            for key, value in pet_fields_schema['properties'].items():
                new_props[key] = value
                if key == 'birthDate':
                    new_props['weight'] = {
                        'type': 'number',
                        'format': 'double',
                        'nullable': True,
                        'description': 'Pet weight in kg'
                    }
            pet_fields_schema['properties'] = new_props

# Write back
with open('src/main/resources/openapi.yml', 'w') as file:
    yaml.dump(openapi, file, default_flow_style=False, indent=2)

print("OpenAPI file updated successfully")
