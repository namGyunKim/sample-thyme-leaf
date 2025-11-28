/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/main/resources/templates/**/*.html",
        "./src/main/resources/static/js/**/*.js"
    ],
    theme  : {
        extend: {
            colors    : {
                primary  : '#3B82F6',
                secondary: '#64748B',
                success  : '#10B981',
                danger   : '#EF4444',
            },
            fontFamily: {
                sans: ['"Noto Sans KR"', 'sans-serif'],
            }
        },
    },
    plugins: [],
}