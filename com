npx spago build && npx purs bundle 'output/**/*.js' -o 'src/main/resources/assets/js/main.js' -m Main

files=("css\/nucleo-icons.css" "css\/black-dashboard.css" "js\/require.js" "js\/main.js" "js\/protobuf.js")
for file in ${files[@]}; do
  cmd="date -r src/main/resources/assets/$file +'%s'"
  v=$(eval "$cmd")
  sed -i '' "s/\($file?\)\([0-9]*\)/\1$v/" src/main/resources/assets/index.html;
done
