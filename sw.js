self.addEventListener('fetch', function(event) {
  if ((event.request.url.indexOf('http') === 0)) {
    console.log('[Service Worker] Fetched resource ' + event.request.url)
    event.respondWith(
      caches.open('portal-cache').then(function (cache) {
        return fetch(event.request).then(function(response) {
          cache.put(event.request, response.clone())
          return response
        }).catch(function() {
          return caches.match(event.request)
        })
      })
    )
  }
})
