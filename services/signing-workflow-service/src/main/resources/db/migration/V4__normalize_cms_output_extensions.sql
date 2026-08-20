UPDATE signing_artifacts
SET output_file_name = regexp_replace(output_file_name, '\.p7c$', '.p7m', 'i')
WHERE output_file_name IS NOT NULL
  AND output_file_name ~* '\.p7c$';
