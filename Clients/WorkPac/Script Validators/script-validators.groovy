// Change Request Priority Validator
issue.priority.name.match('^(Emergency|Major|Normal|Standard)$') != null

// Support Priority Validator
issue.priority.name.match('^(Must|Should|Could|Not Set|)$') == null
