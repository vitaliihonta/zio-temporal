const siteConfig = {
  title: 'ZIO Temporal',
  tagline: 'Build invincible apps with ZIO and Temporal',
  url: 'https://zio-temporal.vhonta.dev',
  baseUrl: '/',
  projectName: 'zio-temporal',
  favicon: 'img/favicon/favicon.ico',
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          path: '../docs/target/mdoc',
          sidebarPath: require.resolve('./sidebars.json'),
        },
        // theme: {
        //   customCss: [require.resolve('./src/css/custom.css')],
        // },
      },
    ],
  ],
  themeConfig: {
    prism: {
      theme: require('prism-react-renderer/themes/nightOwl'),
      additionalLanguages: [
        'java',
        'scala',
      ],
    },
    announcementBar: {
      id: 'support_ukraine',
      content:
        'Support Ukraine 🇺🇦 <a target="_blank" rel="noopener noreferrer" \
          href="http://u24.gov.ua/"> Help Provide Aid to Ukraine</a>.',
      backgroundColor: '#20232a',
      textColor: '#fff',
      isCloseable: false,
    },
    navbar: {
      title: 'ZIO Temporal',
      // logo: {
      //   alt: 'Create React App Logo',
      //   src: 'img/logo.svg',
      // },
      items: [
        { to: 'docs/core/overview', label: 'Documentation', position: 'right' },
        {
           type: 'html',
           value: '<a class="navbar__item navbar__link" rel="nofollow" href="/api/zio/temporal">API Docs</a>',
           position: 'right' 
        },
        {
          href: 'https://github.com/vitaliihonta/zio-temporal',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Info',
          items: [
            {
              label: 'Contacts',
              to: 'docs/contacts',
            },
            {
              label: 'Contribution',
              to: 'docs/contribution'
            }
          ],
        },
      ],
      // logo: {
      //   alt: 'Facebook Open Source Logo',
      //   src: 'img/oss_logo.png',
      // },
      copyright: `Copyright © ${new Date().getFullYear()} Vitalii Honta`,
    }
  }
};

module.exports = siteConfig;