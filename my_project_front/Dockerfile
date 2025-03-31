FROM nginx:stable-alpine
RUN rm -f /usr/share/nginx/html/index.html /user/share/nginx/html/50x.html
COPY . /usr/share/nginx/html/
EXPOSE 80
