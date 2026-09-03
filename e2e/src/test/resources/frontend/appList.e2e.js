// The app grid of the private application. Shipped in the e2e image so that the SPA is fully
// configured: an absent appList.js is fetched and 404s, which pollutes the console assertions.
var appList = [
  {
    name: 'Mail',
    link: 'http://mail.e2e.local',
    icon: '/assets/images/svg/app-mail.svg'
  },
  {
    name: 'Drive',
    link: 'http://drive.e2e.local',
    icon: '/assets/images/svg/app-drive.svg'
  }
]
